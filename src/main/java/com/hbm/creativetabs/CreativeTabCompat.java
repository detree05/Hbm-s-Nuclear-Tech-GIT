package com.hbm.creativetabs;

import java.lang.reflect.Method;

import net.minecraft.creativetab.CreativeTabs;

final class CreativeTabCompat {

	private static final String[] BACKGROUND_METHOD_NAMES = new String[] { "setBackgroundImageName", "func_78025_a" };

	private CreativeTabCompat() { }

	static void setBackgroundImage(CreativeTabs tab, String imageName) {
		if(tab == null || imageName == null) return;

		for(String methodName : BACKGROUND_METHOD_NAMES) {
			try {
				Method m = CreativeTabs.class.getMethod(methodName, String.class);
				m.invoke(tab, imageName);
				return;
			} catch(Throwable ignored) { }
		}
	}
}

