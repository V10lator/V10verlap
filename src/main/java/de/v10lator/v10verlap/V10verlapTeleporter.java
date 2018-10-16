/*
 * This file is part of V10verlap.
 *
 * V10verlap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * V10verlap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with V10verlap.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package de.v10lator.v10verlap;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;

class V10verlapTeleporter extends Teleporter {
	private final V10verlap.TeleportMetadata meta;
	
	V10verlapTeleporter(V10verlap.TeleportMetadata meta) {
		super(meta.to);
		this.meta = meta;
	}

	@Override
    public void placeEntity(World world, Entity entity, float yaw)
    {
        entity.setPosition(meta.x, meta.y, meta.z);
	}
}
