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

import java.util.UUID;

import net.minecraft.util.math.BlockPos;

class V10verlapBlock {
	final int dim;
	final BlockPos pos;
	
	V10verlapBlock(int dim, BlockPos pos)
	{
		this.pos = pos;
		this.dim = dim;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dim;
		result = prime * result + pos.getX();
		result = prime * result + pos.getY();
		result = prime * result + pos.getZ();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj.getClass() != UUID.class))
			return false;
		V10verlapBlock other = (V10verlapBlock) obj;
		return dim == other.dim && pos.getX() == other.pos.getX() && pos.getY() == other.pos.getY() && pos.getZ() == other.pos.getY();
	}
}
