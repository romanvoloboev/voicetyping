package com.romanvoloboev;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class App extends Application {
	private ConfigurableApplicationContext springContext;
	private Parent rootNode;

	public static void main(String[] args) {
		launch(App.class, args);
	}

	@Override
	public void init() throws Exception {
		springContext = SpringApplication.run(App.class);
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main_form.fxml"));
		fxmlLoader.setControllerFactory(springContext::getBean);
		rootNode = fxmlLoader.load();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("My super app");
		primaryStage.setScene(new Scene(rootNode));
		primaryStage.setResizable(false);
		primaryStage.setWidth(800);
		primaryStage.show();
	}

	@Override
	public void stop() throws Exception {
		springContext.stop();
	}
}
