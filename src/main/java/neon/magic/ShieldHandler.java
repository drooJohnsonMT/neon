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

package neon.magic;

import neon.entities.Creature;
import neon.entities.property.Ability;

public class ShieldHandler implements EffectHandler {
	public Ability ability;
	
	public ShieldHandler(Ability ability) {
		this.ability = ability;
	}
	
	public boolean isWeaponEnchantment() {
		return false;
	}

	public boolean isClothingEnchantment() {
		return true;
	}

	public boolean onItem() {
		return false;
	}

	public void addEffect(Spell spell) {
		Creature target = (Creature)spell.getTarget();
		target.getCharacteristicsComponent().addAbility(ability, (int)spell.getMagnitude());
	}

	public void removeEffect(Spell spell) {
		Creature target = (Creature)spell.getTarget();
		target.getCharacteristicsComponent().addAbility(ability, -(int)spell.getMagnitude());
	}

	public void repeatEffect(Spell spell) {
		// geen repeat nodig
	}
}
