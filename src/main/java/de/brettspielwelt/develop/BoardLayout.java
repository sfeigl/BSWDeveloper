package de.brettspielwelt.develop;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;

public class BoardLayout implements LayoutManager {

	@Override
	public void addLayoutComponent(String name, Component comp) {
	}

	@Override
	public void removeLayoutComponent(Component comp) {
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		int consumeHeight = getConsumeHeight(parent);
		return new Dimension(1220,784 + consumeHeight);
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		int consumeHeight = getConsumeHeight(parent);
		return new Dimension(122,78 + consumeHeight);
	}

	@Override
	public void layoutContainer(Container parent) {
		if (parent.getComponentCount() == 0) {
			return;
		}
		Insets parentInsets = parent.getInsets();
		int maxWidth = parent.getWidth() - (parentInsets.left + parentInsets.right);
		int maxHeight = parent.getHeight() - (parentInsets.top + parentInsets.bottom);

		int consumeHeight = getConsumeHeight(parent);
		int boardWidth = maxWidth;
		int boardHeight = (maxHeight-consumeHeight) / parent.getComponentCount();

		int padX = 0;
		int padY = 0;
		double ratio = (double) boardWidth / boardHeight;
		if (ratio >= (1220.0/784.0)) {
			int effectiveWidth = boardHeight * 1220 / 784;
			padX = (boardWidth - effectiveWidth) / 2;
			boardWidth = effectiveWidth;
		} else {
			int effectiveHeight = boardWidth *784 / 1220;
			padY = (boardHeight - effectiveHeight) / 2;
			boardHeight = effectiveHeight;
		}

		int boardX = padX + parentInsets.left;
		int boardY = padY + parentInsets.top;
		for (int i=0;i<parent.getComponentCount();++i) {
			Component component = parent.getComponent(i);
			int bottomHeight = 0;
			if (component instanceof BaseCanvas) {
				BaseCanvas baseCanvas = (BaseCanvas) component;
				Component bottomComponent = baseCanvas.getBottomComponent();
				Dimension dimension = bottomComponent.getPreferredSize();
				bottomHeight = (int) dimension.getHeight();
			}

			component.setBounds(
					new Rectangle(boardX, boardY, boardWidth, boardHeight + bottomHeight)
			);
			boardY += boardHeight + bottomHeight;

		}
	}

	private static int getConsumeHeight(Container parent) {
		int consumeHeight = 0;
		for (int i = 0; i< parent.getComponentCount(); ++i) {
			Component component = parent.getComponent(i);
			if (component instanceof BaseCanvas) {
				BaseCanvas baseCanvas = (BaseCanvas) component;
				Component bottomComponent = baseCanvas.getBottomComponent();
				Dimension dimension = bottomComponent.getPreferredSize();
				consumeHeight += (int) dimension.getHeight();
  			}

		}
		return consumeHeight;
	}
}
