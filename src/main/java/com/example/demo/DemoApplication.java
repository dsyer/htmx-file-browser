package com.example.demo;

import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
	public View home() {
		return JStachioModelView.of(new IndexPage(Folder.cwd()));
	}

}

@JStache(path = "index")
class IndexPage {
	private Node root;

	public IndexPage(Node root) {
		this.root = root;
	}

	public Node getRoot() {
		return root;
	}

	public void setRoot(Node root) {
		this.root = root;
	}
}

class Node implements Comparable<Node> {
	private String name;
	private Node parent;

	public Node(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Node{" +
				"name='" + name + "'" +
				"}";
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	@Override
	public int compareTo(Node o) {
		return this.name.compareTo(o.name);
	}
}

class Folder extends Node {

	public static Folder ROOT = new Folder("/");

	public static Folder cwd() {
		return new Folder(".");
	}

	private SortedSet<Node> children = new TreeSet<>();

	public Folder(String name) {
		super(name);
	}

	public void add(Node child) {
		child.setParent(this);
		children.add(child);
	}

}