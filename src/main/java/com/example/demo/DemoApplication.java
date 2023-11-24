package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.jstach.jstache.JStache;
import io.jstach.jstache.JStachePath;
import io.jstach.opt.spring.webmvc.JStachioModelView;


@SpringBootApplication
@JStachePath(prefix = "templates/", suffix = ".mustache")
public class DemoApplication implements WebMvcConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}

@Controller
class HomeController {
	@GetMapping
	public View home(@ModelAttribute Foo target) {
		return JStachioModelView.of(new IndexPage(target));
	}
	
}

@JStache(path = "index")
class IndexPage {
	private Foo target;
	public IndexPage(Foo target) {
		this.target = target;
	}
	public Foo getTarget() {
		return target;
	}
	public void setTarget(Foo target) {
		this.target = target;
	}
}

class Foo {
	private String value;
	public Foo(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return "Foo{" +
			"value='" + value + "'" +
			"}";
	}
}