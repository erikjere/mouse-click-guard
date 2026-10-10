package com.erikjere.mouseclickguard;

/**
 * {@link ButtonStats} for the left and right button over the same period.
 */
class ButtonPair
{
	private ButtonStats left = new ButtonStats();
	private ButtonStats right = new ButtonStats();

	ButtonStats get(MouseButton button)
	{
		return button == MouseButton.LEFT ? left : right;
	}

	void record(PressEvent event)
	{
		get(event.getButton()).record(event);
	}

	ButtonPair copy()
	{
		ButtonPair copy = new ButtonPair();
		copy.left = left.copy();
		copy.right = right.copy();
		return copy;
	}

	void normalize()
	{
		if (left == null)
		{
			left = new ButtonStats();
		}
		if (right == null)
		{
			right = new ButtonStats();
		}
		left.normalize();
		right.normalize();
	}
}
