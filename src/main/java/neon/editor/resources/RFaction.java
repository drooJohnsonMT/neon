/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - Maarten Driesen
 * 
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neon.editor.resources;

import neon.resources.RData;

import org.jdom2.Element;

public class RFaction extends RData {
	public RFaction(Element e, String... path) {
		super(e.getAttributeValue("id"), path);
	}

	public RFaction(String id, String... path) {
		super(id, path);
	}

	public Element toElement() {
		Element faction = new Element("faction");
		faction.setAttribute("id", id);
		return faction;
	}
}
