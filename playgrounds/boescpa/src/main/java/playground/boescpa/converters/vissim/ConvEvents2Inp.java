/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim;

import org.matsim.api.core.v01.Id;
import playground.boescpa.converters.vissim.tools.InpNetworkMapper;
import playground.boescpa.converters.vissim.tools.InpRouteConverter;
import playground.boescpa.converters.vissim.tools.MsNetworkMapper;
import playground.boescpa.converters.vissim.tools.MsRouteConverter;

import java.util.HashMap;

/**
 * Extends and implements the abstract class ConvEvents for Inp-Files.
 *
 * @author boescpa
 */
public class ConvEvents2Inp extends ConvEvents {

	public ConvEvents2Inp(BaseGridCreator baseGridCreator, NetworkMapper matsimNetworkMapper, NetworkMapper anmNetworkMapper, RouteConverter matsimRouteConverter, RouteConverter anmRouteConverter, TripMatcher tripMatcher) {
		super(baseGridCreator, matsimNetworkMapper, anmNetworkMapper, matsimRouteConverter, anmRouteConverter, tripMatcher);
	}

	public static void main(String[] args) {
		// path2VissimZoneShp = args[0];
		// path2MATSimNetwork = args[1];
		// path2VissimNetwork = args[2];
		// path2EventsFile = args[3];
		// path2VissimRoutesFile = args[4];
		// path2NewVissimRoutesFile = args[5];

		ConvEvents2Anm convEvents2Anm = createDefaultConvEvents2Anm();
		convEvents2Anm.convert(args);
	}

	public static ConvEvents2Anm createDefaultConvEvents2Anm() {
		return new ConvEvents2Anm(new playground.boescpa.converters.vissim.tools.BaseGridCreator(), new MsNetworkMapper(), new InpNetworkMapper(),
				new MsRouteConverter(), new InpRouteConverter(), new playground.boescpa.converters.vissim.tools.TripMatcher());
	}

	@Override
	public void writeAnmRoutes(HashMap<Id, Integer> demandPerVissimTrip, String path2VissimRoutesFile, String path2NewVissimRoutesFile) {

	}
}