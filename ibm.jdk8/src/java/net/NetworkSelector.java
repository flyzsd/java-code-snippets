/*===========================================================================
 * Licensed Materials - Property of IBM
 * "Restricted Materials of IBM"
 * 
 * IBM SDK, Java(tm) Technology Edition, v8
 * (C) Copyright IBM Corp. 1995, 2011. All Rights Reserved
 *
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 *===========================================================================
 */
/*
 * Copyright (c) 1995, 2011, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/**
 * =======================================================================
 * Module Information:
 *
 * DESCRIPTION: This class implements network selection mechanism for
 * Java plain sockets by extending from AbstractNetworkSelector.
 * Configuration file provided against 'com.ibm.net.rdma.conf' property
 * is parsed to prepare the rules database which will be referred to
 * while switching from TCP sockets to RDMA.
 * 
 * This package private final class provides RDMA_CAPABILITY.
 * =======================================================================
 */

package java.net;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;
import java.lang.reflect.Method;
import java.net.InetAddress;
import sun.net.AbstractNetworkSelector;
import sun.net.NetworkProvider;
import sun.net.SocketAction;

import sun.security.action.GetPropertyAction;

final class NetworkSelector extends AbstractNetworkSelector {
	// indicates if the RDMA network provider is enabled
	private static final boolean enabled;
	private static final String osName;
	private static final String osArch;
	// Address which user prefers to override during bind
	private static InetAddress preferredAddress;

	// whether display jsor info messages?
	private static boolean debugInterception = false;

	// rules when the RDMA protocol is used
	private static LinkedList<Rule> bindRules;
	private static LinkedList<Rule> connectRules;
	private static LinkedList<Rule> acceptRules;
	// network provider to name mapping
	private static HashMap<String, NetworkProvider> providerMap;

	/**
	 * Initialize the protocol usage rules
	 */
	private static boolean initializeRules() {
		// flag indicating whether rules are loaded from the configuration file
		boolean rulesLoaded = false;
		// retrieve the configuration file
		String confFile = AccessController.doPrivileged(
				new GetPropertyAction("com.ibm.net.rdma.conf"));
		if (confFile != null) {
			bindRules = new LinkedList<Rule>();
			connectRules = new LinkedList<Rule>();
			acceptRules = new LinkedList<Rule>();
			providerMap = new HashMap<String, NetworkProvider>();
			try {
				loadRulesFromFile(confFile);
				rulesLoaded = true;
			} catch (IOException e) {
				// Ignore...
			}
			if (rulesLoaded) {
				// Register a shutdown hook to cleanup the network resources
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						Iterator<NetworkProvider> it = providerMap.values().iterator();
						while (it.hasNext()) {
							NetworkProvider networkProvider = it.next();
							networkProvider.cleanup();
						}
					}
				});
			} else {
				bindRules = null;
				connectRules = null;
				acceptRules = null;
				providerMap = null;
			}
		}
		return rulesLoaded;
	}

	/*
	 *  this implementation currently supported on all linux variants
	 *  except zLinux
	 */
	static {
		// flag indicating whether rules are initialized
		boolean rulesInited = false;

		GetPropertyAction propAct = new GetPropertyAction("os.name");
		osName = AccessController.doPrivileged(propAct);
		propAct = new GetPropertyAction("os.arch");
		osArch = AccessController.doPrivileged(propAct);

		if (osName.equalsIgnoreCase("linux") && (!osArch.startsWith("s390"))) {
			rulesInited = initializeRules();
		}
		enabled = rulesInited;
		// dump rules if debug flag is set
		if (isDebugOn() && rulesInited) {
			System.out.printf("JSORI:NET: >>>==========%n");
			System.out.printf("%s",printRules(bindRules, acceptRules,
					connectRules, preferredAddress));
			System.out.printf("JSORI:NET: ==========<<<%n");
		}
	}

	/**
	 * Load the specified network provider - currently,
	 * we only check RDMA network provider
	 */
	private static NetworkProvider loadProvider(String name) {
		NetworkProvider provider = null;
		if (providerMap.containsKey(name)) {
			provider = providerMap.get(name);
		} else if (name.equalsIgnoreCase("rdma")) {
			try {
				// Let's rely on the bootstrap class loader to locate
				// and load the RDMANetworkProvider class
				Class netProvider = Class.forName("java.net.RDMANetworkProvider",
						true, null);
				provider = (NetworkProvider)netProvider.newInstance();
				Method intializeProvider = netProvider.getMethod("initialize",
						(Class[])null);
				intializeProvider.invoke(provider, (Object[])null);
				// Invoke the network provider's set preferred address routine
				provider.setPreferredAddress(Host.getIBAddressList(),
						Host.getEthAddressList());
				preferredAddress = provider.getPreferredAddress();

				// Set debug interception flag for JSoR info messages
				debugInterception = provider.isDebugOn();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				return null;
			}
			providerMap.put(name, provider);
		}
		return provider;
	}

	/**
	 * Load rules from the specified configuration file.
	 * 
	 * Each non-blank or non-comment line must have the following format:
	 *   <entry>         = <net-spec> <sp> <connect-entry> | \
	 *   					<accept-entry> | <bind-entry>
	 *   <net-spec>      = "rdma"
	 *   <sp>            = 1*LWSP-char
	 *   <connect-entry> = "connect" <host-spec> <sp> <port-spec>
	 *   <host-spec>     = (hostname | ipaddress ["/" prefix])
	 *   <port-spec>     = ("*" | port)[ "-" ("*" | port) ]
	 *   <accept-entry>  = "accept" <host-spec> <sp> <port-spec> <sp> \
	 *   					("*"| "any" | "all" | <client-spec> \
	 *   					[<sp> <client-spec>])
	 *   <client-spec>   = (hostname | ipaddress ["/" prefix])
	 *   <bind-entry>    = "bind" <host-spec> <sp> <port-spec>
	 * Note:
	 *   1) Comment lines should begin with '#' character.
	 *   2) For connect entry, host and port specifications should refer to
	 *   	the remote host.
	 *   3) For accept entry, host and port specifications should refer to
	 *   	the local host.
	 *   4) For accept entry, all client specifications should refer to
	 *   	remote hosts.
	 *   5) For bind entry, host and port specifications should refer to
	 *   	the local host.
	 *   6) Each hostname or ipaddress specified should be a valid RDMA
	 *   	capable	InfiniBand or ROCE interface address.
	 *   7) In case of bind, hostname or ipaddress could be either null
	 *   	or loopback.
	 *   8) In case of accept, local hostname or ipaddress could be
	 *   	either null or loopback.
	 *   9) In case of connect, remote hostname or ipaddress could be
	 *   	either null or loopback.
	 *  10) Null or loopback addresses will be replaced by the preferred 
	 *  	RDMA capable interface address during the actual bind/connect.
	 *  11) For null IP address use one of '0','0.0','0.0.0', or '0.0.0.0'.
	 *  12) For loopback IP address use '127.0.0.1'.
	 *  13) Server configuration file can have accept and bind entries.
	 *  14) Each accept entry implicitly generates a bind entry as well.
	 *  15) Client configuration file can have connect and bind entries.
	 */
	private static void loadRulesFromFile(String file) throws IOException {
		Scanner scanner = new Scanner(new File(file));
		try {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();

				// skip blank lines and comments
				if (line.length() == 0 || line.charAt(0) == '#') {
					continue;
				}

				// must have at least 4 fields
				String[] s = line.split("\\s+");
				if (s.length < 4) {
					fail("Malformed line '%s'", line);
				}

				// first field is the network identifier
				NetworkProvider provider = loadProvider(s[0]);
				if (provider == null) {
					fail("Network provider for '%s' could not be loaded", s[0]);
				}

				// second field is the action ("connect" or "accept" or "bind")
				SocketAction action = SocketAction.parse(s[1]);
				if (action == null) {
					fail("SocketAction '%s' not recognized", s[1]);
				}

				if (action == SocketAction.ACCEPT && s.length < 5) {
					fail("Less than expected number of arguments '%s'", line);
				}

				if ((action == SocketAction.BIND || action == SocketAction.CONNECT)
						&& s.length > 4) {
					fail("Greater than expected number of arguments '%s'", line);
				}

				// third field is the host specification
				Host[] primaryHosts = Host.parse(action, s[2], false);
				if (primaryHosts == null) {
					fail("Primary host field '%s' not valid", s[2]);
				}

				// fourth field is the port specification
				PortRange range = null;
				try {
					range = new PortRange(s[3]);
				} catch (NumberFormatException nfe) {
					fail("Malformed port range '%s'", s[3]);
				}

				/* fifth and above fields (if exist) contain client addresses
				 * create one rule for each host-client combination
				 */
				if (action == SocketAction.ACCEPT) {
					for (int i = 4; i < s.length; i++) {
						Host[] remoteHost = Host.parse(action, s[i], true);
						if (remoteHost != null) {
							for (int p = 0; p < primaryHosts.length; p++) {
								for (int j = 0; j < remoteHost.length; j++) {
									addRule(bindRules, acceptRules, connectRules,
											new HostPortRule(action, primaryHosts[p],
													range, remoteHost[j], provider));
								}
							}
						}
					}
				} else {
					// client address portion is null
					for (int p = 0; p < primaryHosts.length; p++) {
						addRule(bindRules, acceptRules, connectRules,
								new HostPortRule(action, primaryHosts[p],
										range, null, provider));
					}
				}
			}
		} finally {
			scanner.close();
		}
	}


	/**
	 * Public interface to retrieve the underlying network provider
	 */
	static NetworkProvider getNetworkProvider(SocketAction action,
			InetAddress primaryHost, int port, InetAddress secondaryHost) {
		return (enabled) ? getNetworkProvider(bindRules, acceptRules,
				connectRules, action, primaryHost, port,
				secondaryHost) : null;
	}
	
	/**
	 * Is debug info flag on?
	 */
	static boolean isDebugOn() {
		return debugInterception;
	}
}
