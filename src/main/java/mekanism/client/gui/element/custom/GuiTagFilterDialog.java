package mekanism.client.gui.element.custom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.lwjgl.glfw.GLFW;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.button.TranslationButton;
import mekanism.client.gui.element.slot.GuiSequencedSlotDisplay;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.TagCache;
import mekanism.common.content.qio.filter.QIOFilter;
import mekanism.common.content.qio.filter.QIOTagFilter;
import mekanism.common.content.transporter.TransporterFilter;
import mekanism.common.network.PacketEditFilter;
import mekanism.common.network.PacketNewFilter;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.ITileFilterHolder;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

public class GuiTagFilterDialog extends GuiFilterDialog<QIOTagFilter> {

    protected MekanismButton checkboxButton;
    protected TextFieldWidget text;
    protected GuiSequencedSlotDisplay slotDisplay;

    private <TILE extends TileEntityMekanism & ITileFilterHolder<QIOFilter<?>>>
    GuiTagFilterDialog(IGuiWrapper gui, int x, int y, TILE tile, QIOTagFilter origFilter) {
        super(gui, x, y, 152, 90, MekanismLang.TAG_FILTER.translate(), origFilter);

        addChild(new GuiSlot(SlotType.NORMAL, gui, relativeX + 7, relativeY + 18).setRenderHover(true));
        addChild(new GuiInnerScreen(gui, relativeX + 29, relativeY + 18, width - 29 - 7, 43, () -> {
            List<ITextComponent> list = new ArrayList<>();
            list.add(MekanismLang.STATUS.translate(status));
            list.add(MekanismLang.TAG_FILTER_TAG.translate(filter.getTagName()));
            return list;
        }).clearFormat());
        addChild(new TranslationButton(gui, gui.getLeft() + relativeX + width / 2 - 61, gui.getTop() + relativeY + 63, 60, 20, isNew ? MekanismLang.BUTTON_CANCEL : MekanismLang.BUTTON_DELETE, () -> {
            if (origFilter != null) {
                Mekanism.packetHandler.sendToServer(new PacketEditFilter(tile.getPos(), true, origFilter, null));
            }
            gui.removeElement(this);
        }));
        addChild(new TranslationButton(gui, gui.getLeft() + relativeX + width / 2 + 1, gui.getTop() + relativeY + 63, 60, 20, MekanismLang.BUTTON_SAVE, () -> {
            if (!text.getText().isEmpty()) {
                setText();
            }
            if (filter.getTagName() != null && !filter.getTagName().isEmpty()) {
                if (isNew) {
                    Mekanism.packetHandler.sendToServer(new PacketNewFilter(tile.getPos(), filter));
                } else {
                    Mekanism.packetHandler.sendToServer(new PacketEditFilter(tile.getPos(), false, origFilter, filter));
                }
                gui.removeElement(this);
            } else {
                status = MekanismLang.TAG_FILTER_NO_TAG.translateColored(EnumColor.DARK_RED);
                ticker = 20;
            }
        }));
        addChild(slotDisplay = new GuiSequencedSlotDisplay(gui, relativeX + 8, relativeY + 19, this::getRenderStacks).setZOffset(200));

        text = new TextFieldWidget(getFont(), gui.getLeft() + relativeX + 31, gui.getTop() + relativeY + 47, width - 31 - 9 - 12, 12, "");
        text.setMaxStringLength(TransporterFilter.MAX_LENGTH);
        text.setEnabled(true);
        text.setFocused2(true);

        addChild(new MekanismImageButton(gui, gui.getLeft() + relativeX + width - 8 - 12, gui.getTop() + relativeY + 47, 12, MekanismUtils.getResource(ResourceType.GUI_BUTTON, "checkmark.png"),
            this::setText));

        if (filter.getTagName() != null && !filter.getTagName().isEmpty()) {
            slotDisplay.updateStackList();
        }
    }

    public static <TILE extends TileEntityMekanism & ITileFilterHolder<QIOFilter<?>>> GuiTagFilterDialog create(IGuiWrapper gui, TILE tile) {
        return new GuiTagFilterDialog(gui, gui.getWidth() / 2 - 152 / 2, 15, tile, null);
    }

    public static <TILE extends TileEntityMekanism & ITileFilterHolder<QIOFilter<?>>> GuiTagFilterDialog edit(IGuiWrapper gui, TILE tile, QIOTagFilter filter) {
        return new GuiTagFilterDialog(gui, gui.getWidth() / 2 - 152 / 2, 15, tile, filter);
    }

    @Override
    public QIOTagFilter createNewFilter() {
        return new QIOTagFilter();
    }

    @Override
    public void renderBackgroundOverlayPost(int mouseX, int mouseY) {
        super.renderBackgroundOverlayPost(mouseX, mouseY);
        text.renderButton(mouseX, mouseY, 0);
    }

    protected void setText() {
        String name = text.getText();
        if (name.isEmpty()) {
            status = MekanismLang.TAG_FILTER_NO_TAG.translateColored(EnumColor.DARK_RED);
            ticker = 20;
            return;
        } else if (name.equals(filter.getTagName())) {
            status = MekanismLang.TAG_FILTER_SAME_TAG.translateColored(EnumColor.DARK_RED);
            ticker = 20;
            return;
        }
        filter.setTagName(name);
        slotDisplay.updateStackList();
        text.setText("");
    }

    @Override
    public void tick() {
        super.tick();
        text.tick();
        if (ticker > 0) {
            ticker--;
        } else {
            status = MekanismLang.STATUS_OK.translateColored(EnumColor.DARK_GREEN);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (text.canWrite()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                //Manually handle hitting escape making the field lose focus
                text.setFocused2(false);
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
                setText();
                return true;
            }
            text.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int keyCode) {
        if (text.canWrite()) {
            if (Character.isLetter(c) || Character.isDigit(c) || TransporterFilter.SPECIAL_CHARS.contains(c) || c == ':' || c == '/') {
                return text.charTyped(c, keyCode);
            }
            return false;
        }
        return super.charTyped(c, keyCode);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean ret = text.mouseClicked(mouseX, mouseY, button);
        return ret || super.mouseClicked(mouseX, mouseY, button);
    }

    private List<ItemStack> getRenderStacks() {
        if (filter.getTagName() == null || filter.getTagName().isEmpty()) {
            return Collections.emptyList();
        }
        return TagCache.getItemTagStacks(filter.getTagName());
    }
}
