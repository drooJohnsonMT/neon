/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2012 - mdriesen
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

package neon.core.handlers;

import java.awt.Rectangle;
import neon.core.Engine;
import neon.core.event.CombatEvent;
import neon.core.event.MagicEvent;
import neon.entities.Creature;
import neon.entities.Item;
import neon.entities.Weapon;
import neon.entities.components.HealthComponent;
import neon.entities.property.Slot;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.References;

/**
 * This class handles combat between two creatures. There are four possible outcomes of 
 * a fight:
 * <ul>
 * 	<li>ATTACK - a succesful attack</li>
 * 	<li>DIE - succesful attack, the target died</li>
 * 	<li>DODGE- the target dodged the attack</li>
 * 	<li>BLOCK - the target blocked the attack</li>
 * </ul>
 * 
 * @author mdriesen
 */
@Listener(references = References.Strong)	// strong, om gc te vermijden
public class CombatHandler {
	@Handler public void handleCombat(CombatEvent ce) {
		if(!ce.isFinished()) {
			int result = 0;
			switch(ce.getType()) {
			case CombatEvent.SHOOT:
				result = shoot(ce.getAttacker(), ce.getDefender());
				break;
			case CombatEvent.FLING:
				result = fling(ce.getAttacker(), ce.getDefender());
				break;
			default:
				result = fight(ce.getAttacker(), ce.getDefender());
				break;
			}
			Engine.post(new CombatEvent(ce.getAttacker(), ce.getDefender(), result));
		}
	}

	/*
	 * This method lets two creatures fight.
	 *
	 * @param attacker the attacking creature
	 * @param defender the defending creature
	 * @return the outcome of the fight
	 */
	private int fight(Creature attacker, Creature defender) {
		long uid = attacker.getInventoryComponent().get(Slot.WEAPON);
		Weapon weapon = (Weapon)Engine.getStore().getEntity(uid);
		return fight(attacker, defender, weapon);
	}

	/*
	 * Lets a creature shoot at a creature. The ammo used to shoot with is
	 * removed from the attacker's inventory.
	 * 
	 * @param shooter	the attacking creature
	 * @param target	the target creature
	 * @return			the outcome of the fight
	 */
	private int shoot(Creature shooter, Creature target) {
		// damage is gemiddelde van pijl en boog (Creature.getAV)
		Weapon ammo = (Weapon)Engine.getStore().getEntity(shooter.getInventoryComponent().get(Slot.AMMO));
		InventoryHandler.removeItem(shooter, ammo.getUID());
		for(long uid : shooter.getInventoryComponent()) {
			Item item = (Item)Engine.getStore().getEntity(uid);
			if(item.getID().equals(ammo.getID())) {
				InventoryHandler.equip(item, shooter);
				break;
			}
		}
		
		long uid = shooter.getInventoryComponent().get(Slot.WEAPON);
		Weapon weapon = (Weapon)Engine.getStore().getEntity(uid);
		return fight(shooter, target, weapon);
	}
	
	/*
	 * Lets a humanoid throw something at a creature. The weapon thrown is removed
	 * from the attacker's inventory.
	 * 
	 * @param thrower	the attacking creature
	 * @param target	the target creature
	 * @return			the outcome of the fight
	 */
	private int fling(Creature thrower, Creature target) {
		Weapon weapon = (Weapon)Engine.getStore().getEntity(thrower.getInventoryComponent().get(Slot.AMMO));
		InventoryHandler.removeItem(thrower, weapon.getUID());
		for(long uid : thrower.getInventoryComponent()) {
			Item item = (Item)Engine.getStore().getEntity(uid);
			if(item.getID().equals(weapon.getID())) {
				InventoryHandler.equip(item, thrower);
				break;
			}
		}
		return fight(thrower, target, weapon);
	}
	
	private int fight(Creature attacker, Creature defender, Weapon weapon) {
		// aanvaller bepaalt een attack value (hangt af van dex)
		int attack = CombatUtils.attack(attacker);
		
		int result;
		
		// verdediger kijkt of hij kan dodgen of blocken
		if(CombatUtils.dodge(defender) < attack) {
			if(CombatUtils.block(defender) < attack) {
				if(weapon != null) {
					weapon.setState(weapon.getState() - 1);
				}
				
				// Attack Value, afhankelijk van wapen, skill en str
				int AV = CombatUtils.getAV(attacker);
				// defense value, afhankelijk van armor, skill
				int DV = CombatUtils.getDV(defender);
				
				// altijd minimum 1 damage
				HealthComponent health = defender.getHealthComponent();
				health.heal(Math.min(-1, -(int)((AV - DV)/(DV + 1))));
				
				// enchanted weapon spell casten
				if(weapon != null && weapon.getMagicComponent().getSpell() != null) {
					Rectangle bounds = defender.getShapeComponent();
					Engine.post(new MagicEvent.ItemOnPoint(this, attacker, weapon, bounds.getLocation()));
				}
				
				// berichten bepalen
				if(health.getHealth() < 0) {
					result = CombatEvent.DIE;
				} else {
					result = CombatEvent.ATTACK;
				}
			} else {	// geblockt
				result = CombatEvent.BLOCK;
			}
		} else {	// gedodged
			result = CombatEvent.DODGE;
		}		

		return result;
	}
}
