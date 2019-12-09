/*******************************************************************************
 * Copyright (c) 2012 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.xml.internal.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SPDXLicense {

  private static final List<SPDXLicense> LICENSES;

  public static final String BASEURL = "http://www.spdx.org/licenses/"; //$NON-NLS-1$

  private final String name;

  private final String id;

  private SPDXLicense(String name, String id) {
    this.name = name;
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public String getURL() {
    return BASEURL + id;
  }

  public static List<SPDXLicense> getStandardLicenses() {
    return LICENSES;
  }

  static {
    ArrayList<SPDXLicense> licenses = new ArrayList<SPDXLicense>();

    // SPDX License List v1.13 
    // http://spdx.org/wiki/spdx-license-list-working-version
    licenses.add(new SPDXLicense("Academic Free License v1.1", "AFL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Academic Free License v1.2", "AFL-1.2")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Academic Free License v2.0", "AFL-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Academic Free License v2.1", "AFL-2.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Academic Free License v3.0", "AFL-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Adaptive Public License 1.0", "APL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("ANTLR Software Rights Notice", "ANTLR-PD")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Apache License 1.0", "Apache-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Apache License 1.1", "Apache-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Apache License 2.0", "Apache-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Apple Public Source License 1.0", "APSL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Apple Public Source License 1.1", "APSL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Apple Public Source License 1.2", "APSL-1.2")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Apple Public Source License 2.0", "APSL-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Artistic License 1.0", "Artistic-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Artistic License 2.0", "Artistic-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Attribution Assurance License", "AAL")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Boost Software License 1.0", "BSL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("BSD 2-clause \"Simplified\" or \"FreeBSD\" License", "BSD-2-Clause")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("BSD 3-clause \"New\" or \"Revised\" License", "BSD-3-Clause")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("BSD 4-clause \"Original\" or \"Old\" License", "BSD-4-Clause")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("CeCILL Free Software License Agreement v1.0", "CECILL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("CeCILL Free Software License Agreement v1.1", "CECILL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("CeCILL Free Software License Agreement v2.0", "CECILL-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("CeCILL-B Free Software License Agreement", "CECILL-B")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("CeCILL-C Free Software License Agreement", "CECILL-C")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Clarified Artistic License", "ClArtistic")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Common Development and Distribution License 1.0", "CDDL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Common Public Attribution License 1.0 ", "CPAL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Common Public License 1.0", "CPL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Computer Associates Trusted Open Source License 1.1", "CATOSL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution 1.0", "CC-BY-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution 2.0", "CC-BY-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution 2.5", "CC-BY-2.5")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution 3.0", "CC-BY-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution No Derivatives 1.0", "CC-BY-ND-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution No Derivatives 2.0", "CC-BY-ND-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution No Derivatives 2.5", "CC-BY-ND-2.5")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution No Derivatives 3.0", "CC-BY-ND-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial 1.0", "CC-BY-NC-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial 2.0", "CC-BY-NC-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial 2.5", "CC-BY-NC-2.5")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial 3.0", "CC-BY-NC-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial No Derivatives 1.0", "CC-BY-NC-ND-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial No Derivatives 2.0", "CC-BY-NC-ND-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial No Derivatives 2.5", "CC-BY-NC-ND-2.5")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial No Derivatives 3.0", "CC-BY-NC-ND-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial Share Alike 1.0", "CC-BY-NC-SA-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial Share Alike 2.0", "CC-BY-NC-SA-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial Share Alike 2.5", "CC-BY-NC-SA-2.5")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Non Commercial Share Alike 3.0", "CC-BY-NC-SA-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Share Alike 1.0", "CC-BY-SA-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Share Alike 2.0", "CC-BY-SA-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Share Alike 2.5", "CC-BY-SA-2.5")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Attribution Share Alike 3.0", "CC-BY-SA-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Creative Commons Zero v1.0 Universal", "CC0-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("CUA Office Public License v1.0", "CUA-OPL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Eclipse Public License 1.0", "EPL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("eCos license version 2.0", "eCos-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Educational Community License v1.0", "ECL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Educational Community License v2.0", "ECL-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Eiffel Forum License v1.0", "EFL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Eiffel Forum License v2.0", "EFL-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Entessa Public License", "Entessa")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Erlang Public License v1.1", "ErlPL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("EU DataGrid Software License", "EUDatagrid")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("European Union Public License 1.0", "EUPL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("European Union Public License 1.1", "EUPL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Fair License", "Fair")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Frameworx Open License 1.0", "Frameworx-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU Affero General Public License v3", "AGPL-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU Free Documentation License v1.1", "GFDL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU Free Documentation License v1.2", "GFDL-1.2")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU Free Documentation License v1.3", "GFDL-1.3")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU General Public License v1.0 only", "GPL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU General Public License v1.0 or later", "GPL-1.0+")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU General Public License v2.0 only", "GPL-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU General Public License v2.0 or later", "GPL-2.0+")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense(
        "GNU General Public License v2.0 w/Autoconf exception", "GPL-2.0-with-autoconf-exception")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU General Public License v2.0 w/Bison exception", "GPL-2.0-with-bison-exception")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense(
        "GNU General Public License v2.0 w/Classpath exception", "GPL-2.0-with-classpath-exception")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU General Public License v2.0 w/Font exception", "GPL-2.0-with-font-exception")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense(
        "GNU General Public License v2.0 w/GCC Runtime Library exception", "GPL-2.0-with-GCC-exception")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU General Public License v3.0 only", "GPL-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU General Public License v3.0 or later", "GPL-3.0+")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense(
        "GNU General Public License v3.0 w/Autoconf exception", "GPL-3.0-with-autoconf-exception")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense(
        "GNU General Public License v3.0 w/GCC Runtime Library exception", "GPL-3.0-with-GCC-exception")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU Lesser General Public License v2.1 only", "LGPL-2.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU Lesser General Public License v2.1 or later", "LGPL-2.1+")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU Lesser General Public License v3.0 only", "LGPL-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU Lesser General Public License v3.0 or later", "LGPL-3.0+")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU Library General Public License v2 only", "LGPL-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("GNU Library General Public License v2 or later", "LGPL-2.0+")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("gSOAP Public License v1.b", "gSOAP-1.3b")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Historic Permission Notice and Disclaimer", "HPND")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("IBM Public License v1.0", "IPL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("IPA Font License", "IPA")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("ISC License (Bind, DHCP Server)", "ISC")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("LaTeX Project Public License v1.0", "LPPL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("LaTeX Project Public License v1.1", "LPPL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("LaTeX Project Public License v1.2", "LPPL-1.2")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("LaTeX Project Public License v1.3c", "LPPL-1.3c")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("libpng License", "Libpng")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Lucent Public License v1.02 (Plan9)", "LPL-1.02")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Microsoft Public License", "MS-PL")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Microsoft Reciprocal License", "MS-RL")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("MirOS Licence", "MirOS")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("MIT license (also X11)", "MIT")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Motosoto License", "Motosoto")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Mozilla Public License 1.0", "MPL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Mozilla Public License 1.1 ", "MPL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Multics License", "Multics")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("NASA Open Source Agreement 1.3", "NASA-1.3")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Naumen Public License", "Naumen")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Nethack General Public License", "NGPL")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Nokia Open Source License", "Nokia")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Non-Profit Open Software License 3.0", "NPOSL-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("NTP License", "NTP")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("OCLC Research Public License 2.0", "OCLC-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("ODC Open Database License v1.0", "ODbL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("ODC Public Domain Dedication & License 1.0", "PDDL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Open Group Test Suite License", "OGTSL")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Open Software License 1.0", "OSL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Open Software License 2.0", "OSL-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Open Software License 3.0", "OSL-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("OpenLDAP Public License v2.8", "OLDAP-2.8")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("OpenSSL License", "OpenSSL")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("PHP License v3.0", "PHP-3.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("PostgreSQL License", "PostgreSQL")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Python License 2.0", "Python-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Q Public License 1.0", "QPL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("RealNetworks Public Source License v1.0", "RPSL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Reciprocal Public License 1.5 ", "RPL-1.5")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Red Hat eCos Public License v1.1", "RHeCos-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Ricoh Source Code Public License", "RSCPL")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Ruby License", "Ruby")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Sax Public Domain Notice", "SAX-PD")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("SIL Open Font License 1.1", "OFL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Simple Public License 2.0", "SimPL-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Sleepycat License", "Sleepycat")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("SugarCRM Public License v1.1.3", "SugarCRM-1.1.3")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Sun Public License v1.0", "SPL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Sybase Open Watcom Public License 1.0", "Watcom-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("University of Illinois/NCSA Open Source License", "NCSA")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Vovida Software License v1.0", "VSL-1.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("W3C Software and Notice License", "W3C")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("wxWindows Library License", "WXwindows")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("X.Net License", "Xnet")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("XFree86 License 1.1", "XFree86-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Yahoo! Public License v1.1", "YPL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Zimbra Publice License v1.3", "Zimbra-1.3")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("zlib License", "Zlib")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Zope Public License 1.1", "ZPL-1.1")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Zope Public License 2.0", "ZPL-2.0")); //$NON-NLS-1$ //$NON-NLS-2$
    licenses.add(new SPDXLicense("Zope Public License 2.1", "ZPL-2.1")); //$NON-NLS-1$ //$NON-NLS-2$

    LICENSES = Collections.unmodifiableList(licenses);
  }

}
