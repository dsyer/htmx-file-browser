package com.example.demo;

import java.io.File;
import java.util.Base64;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.jstach.jstache.JStache;
import io.jstach.jstache.JStacheLambda;
import io.jstach.jstache.JStacheLambda.Raw;
import io.jstach.jstache.JStachePath;
import io.jstach.jstachio.JStachio;
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

	@HxRequest
	@GetMapping("/open/{path}")
	public View open(@PathVariable String path) {
		return JStachioModelView.of(new Folder(Node.decode(path)).doOpen());
	}

	@HxRequest
	@GetMapping("/close/{path}")
	public View close(@PathVariable String path) {
		return JStachioModelView.of(new Folder(Node.decode(path)));
	}
}

@JStache(path = "index")
class IndexPage {
	private Folder root;

	public IndexPage(Folder root) {
		this.root = root;
	}

	public Folder getRoot() {
		return root;
	}

	public void setRoot(Folder root) {
		this.root = root;
	}

	@Raw
	@JStacheLambda
	public String html(Node node) {
		StringBuilder appendable = new StringBuilder();
		JStachio.render(node, appendable);
		return appendable.toString();
	}
}

@JStache(path = "file")
class Node implements Comparable<Node> {
	private String name;
	private Node parent;

	public boolean isLeaf() {
		return true;
	}

	public static String decode(String path) {
		return new String(Base64.getDecoder().decode(path));
	}

	public static String encode(String path) {
		return new String(Base64.getEncoder().encode(path.getBytes()));
	}

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
		return (this.isLeaf() != o.isLeaf()) ? this.isLeaf() ? 1 : -1 : this.name.compareTo(o.name);
	}

	@Raw
	@JStacheLambda
	public String html(Node node) {
		StringBuilder appendable = new StringBuilder();
		JStachio.render(node, appendable);
		return appendable.toString();
	}

}

@JStache(path = "folder")
class Folder extends Node {

	public static Folder ROOT = new Folder("/");

	private boolean open = false;

	public static Folder cwd() {
		return new Folder(".");
	}

	public Folder doOpen() {
		setOpen(true);
		File top = new File(getPath());
		if (top.listFiles() == null) {
			return this;
		}
		for (File file : top.listFiles()) {
			if (file.isDirectory()) {
				add(new Folder(file.getName()));
			} else {
				add(new Node(file.getName()));
			}
		}
		return this;
	}

	private Set<Node> children = new TreeSet<>();

	public Folder(String name) {
		super(StringUtils.getFilename(name));
		String[] tokens = StringUtils.tokenizeToStringArray(name, "/");
		if (tokens.length <= 1) {
			return;
		}
		Folder folder = this;
		for (int i=tokens.length - 1; i-->0;) {
			folder.setParent(new Folder(tokens[i]));
			folder = (Folder) folder.getParent();
		}
	}

	public String getPath() {
		if (getParent() == null) {
			return getName();
		}
		return ((Folder) getParent()).getPath() + "/" + getName();
	}

	public String getEncoded() {
		return encode(getPath());
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	public void add(Node child) {
		child.setParent(this);
		children.add(child);
	}

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public Set<Node> getChildren() {
		return children;
	}

	@Raw
	@JStacheLambda
	public String html(Node node) {
		StringBuilder appendable = new StringBuilder();
		JStachio.render(node, appendable);
		return appendable.toString();
	}

}