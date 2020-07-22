package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockFactory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.event.block.DoorToggleEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Faceable;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by Pub4Game on 26.12.2015.
 */
public class BlockBehaviorTrapdoor extends BlockBehaviorTransparent implements Faceable {

    public static final int TRAPDOOR_OPEN_BIT = 0x08;
    public static final int TRAPDOOR_TOP_BIT = 0x04;

    private static final AxisAlignedBB[] boundingBoxDamage = new AxisAlignedBB[16];

    protected BlockColor blockColor;

    public BlockBehaviorTrapdoor(Identifier id) {
        this(id, BlockColor.WOOD_BLOCK_COLOR);
    }

    public BlockBehaviorTrapdoor(Identifier id, BlockColor blockColor) {
        super(id);
        this.blockColor = blockColor;
    }

    static {
        for (int damage = 0; damage < 16; damage++) {
            AxisAlignedBB bb;
            float f = 0.1875f;
            if ((damage & TRAPDOOR_TOP_BIT) > 0) {
                bb = new SimpleAxisAlignedBB(
                        0,
                        1 - f,
                        0,
                        1,
                        1,
                        1
                );
            } else {
                bb = new SimpleAxisAlignedBB(
                        0,
                        0,
                        0,
                        1,
                        0 + f,
                        1
                );
            }
            if ((damage & TRAPDOOR_OPEN_BIT) > 0) {
                if ((damage & 0x03) == 0) {
                    bb.setBounds(
                            0,
                            0,
                            1 - f,
                            1,
                            1,
                            1
                    );
                } else if ((damage & 0x03) == 1) {
                    bb.setBounds(
                            0,
                            0,
                            0,
                            1,
                            1,
                            0 + f
                    );
                }
                if ((damage & 0x03) == 2) {
                    bb.setBounds(
                            1 - f,
                            0,
                            0,
                            1,
                            1,
                            1
                    );
                }
                if ((damage & 0x03) == 3) {
                    bb.setBounds(
                            0,
                            0,
                            0,
                            0 + f,
                            1,
                            1
                    );
                }
            }
            boundingBoxDamage[damage] = bb;
        }
    }

    @Override
    public float getHardness() {
        return 3;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public float getResistance() {
        return 15;
    }

    private AxisAlignedBB getRelativeBoundingBox() {
        return boundingBoxDamage[this.getMeta()];
    }

    @Override
    public float getMinX() {
        return this.getX() + getRelativeBoundingBox().getMinX();
    }

    @Override
    public float getMaxX() {
        return this.getX() + getRelativeBoundingBox().getMaxX();
    }

    @Override
    public float getMinY() {
        return this.getY() + getRelativeBoundingBox().getMinY();
    }

    @Override
    public float getMaxY() {
        return this.getY() + getRelativeBoundingBox().getMaxY();
    }

    @Override
    public float getMinZ() {
        return this.getZ() + getRelativeBoundingBox().getMinZ();
    }

    @Override
    public float getMaxZ() {
        return this.getZ() + getRelativeBoundingBox().getMaxZ();
    }

    public static BlockFactory factory(BlockColor blockColor) {
        return identifier -> new BlockBehaviorTrapdoor(identifier, blockColor);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_REDSTONE) {
            if ((!this.isOpen() && this.level.isBlockPowered(this.getPosition())) || (this.isOpen() && !this.level.isBlockPowered(this.getPosition()))) {
                this.level.getServer().getPluginManager().callEvent(new BlockRedstoneEvent(this, isOpen() ? 15 : 0, isOpen() ? 0 : 15));
                this.setMeta(this.getMeta() ^ TRAPDOOR_OPEN_BIT);
                this.level.setBlock(this.getPosition(), this, true);
                this.level.addSound(this.getPosition(), isOpen() ? Sound.RANDOM_DOOR_OPEN : Sound.RANDOM_DOOR_CLOSE);
                return type;
            }
        }

        return 0;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id, 0);
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (toggle(player)) {
            this.level.addSound(this.getPosition(), isOpen() ? Sound.RANDOM_DOOR_OPEN : Sound.RANDOM_DOOR_CLOSE);
            return true;
        }
        return false;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        BlockFace facing;
        boolean top;
        int meta = 0;

        if (face.getAxis().isHorizontal() || player == null) {
            facing = face;
            top = clickPos.getY() > 0.5f;
        } else {
            facing = player.getDirection().getOpposite();
            top = face != BlockFace.UP;
        }

        int[] faces = {2, 1, 3, 0};
        int faceBit = faces[facing.getHorizontalIndex()];
        meta |= faceBit;

        if (top) {
            meta |= TRAPDOOR_TOP_BIT;
        }
        this.setMeta(meta);
        this.getLevel().setBlock(blockState.getPosition(), this, true, true);
        return true;
    }

    public boolean toggle(Player player) {
        DoorToggleEvent ev = new DoorToggleEvent(this, player);
        getLevel().getServer().getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return false;
        }
        this.setMeta(this.getMeta() ^ TRAPDOOR_OPEN_BIT);
        getLevel().setBlock(this.getPosition(), this, true);
        return true;
    }

    public boolean isOpen() {
        return (this.getMeta() & TRAPDOOR_OPEN_BIT) != 0;
    }

    public boolean isTop() {
        return (this.getMeta() & TRAPDOOR_TOP_BIT) != 0;
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getMeta() & 0x07);
    }

    @Override
    public BlockColor getColor() {
        return this.blockColor;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}