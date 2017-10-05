package com.romanvoloboev;

import com.romanvoloboev.view.MainFormView;
import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication extends AbstractJavaFxApplicationSupport{

	public static void main(String[] args) {
		launchApp(DemoApplication.class, MainFormView.class, args);
	}
}
