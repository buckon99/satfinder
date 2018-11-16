/***************************************************************************
 *   Copyright (C) 2011 by Paul Lutus                                      *
 *   http://arachnoid.com/administration                                   *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

package com.jbuckon.satfinder.ar;

final public class SatDescription {
	final double rToD = 180.0 / Math.PI;
	final double dToR = Math.PI / 180.0;
	int index;
	String satName;
	String satTitle;
	boolean isHeader = false;
	double lat = -100, lng = 0;
	double satLng;
	double targetAzTrue = 0;
	double targetAzMag = 0;
	double targetEl = 0;
	double targetSkew0 = 0;
	double targetSkew90 = 0;
    double rse = 1.0/6.6107; // ratio synchronous orbit/earth radius

	// error indication: lat = -100
	public SatDescription(String record, int i, double magDec, double lat,
			double lng) {
		this.lat = lat;
		this.lng = lng;
		index = i;
		if (isHeader = record.matches("^\\*\\*\\*.*")) {
			satName = record.replaceFirst("^\\*+\\s*", "");
			satTitle = "(no target selected)";
		} else {
			satName = record.replaceFirst("(.*),.*", "$1");
			satTitle = satName;
			String slng = record.replaceFirst(".*,(.*)", "$1");
			satLng = Double.parseDouble(slng);
			if (lat != -100) {
				Complex azEl;
				// double azm, sk0, sk90;
				azEl = computePos(lat, lng, satLng);
				targetEl = azEl.y();
				targetSkew0 = computeSkew(lat, lng, satLng);
				targetAzTrue = azEl.x();
				targetAzTrue = (targetAzTrue < 0) ? 360 + targetAzTrue
						: targetAzTrue;
				targetAzMag = (targetAzTrue - magDec * rToD + 720) % 360;
				targetSkew90 = 90 + targetSkew0;
			}
		}
	}

	/*
	 * 
	 * ComputePos(input: (earth) lat (deg), lng (deg), (satellite) satlng (deg),
	 * output: Complex(x = az true (deg), y = el (deg))
	 * 
	 * az format: North = 0, East = 90, South = 180, West = 270 el format:
	 * Horizontal 0, Vertical 90
	 */

    private Complex computePos(double lat, double lng, double satLng) {
        double dlng = (lng - satLng);
        double dlngr = dlng * dToR;
        double az = Math.atan2( Math.tan(dlngr),Math.sin(lat * dToR)) * rToD;
        az = (az < 0)?180 + az:az;
        double dd = ((dlng+360) % 360);
        if(dd > 180) az += 180;
        az = (180 + az) % 360;
        double clng = Math.cos(dlngr);
        double clat = Math.cos(lat * dToR);
        double v1 = clat * clng - rse;
        double v2 = Math.sqrt(1 - clat * clat * clng * clng);
        double el = Math.atan2(v1, v2) * rToD;
        return new Complex(az, el);
    }

	private double computeSkew(double lat, double lng, double satLng) {
		double alat = Math.abs(lat);
		double dlngr = (satLng - lng) * dToR;
		double v = (Math.atan2(Math.tan(alat * dToR), Math.sin(dlngr)) * rToD) - 90.0;
		return (lat < 0) ? -v : v;

	}

	public String toString() {
        return satTitle;
    }


}
