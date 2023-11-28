package com.example.demo;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.TimeZone;
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
import io.jstach.jstache.JStacheFormatterTypes;
import io.jstach.jstache.JStachePath;
import io.jstach.opt.spring.webmvc.JStachioModelView;

@SpringBootApplication
@JStachePath(prefix = "templates/", suffix = ".mustache")
@JStacheFormatterTypes(types = LocalDateTime.class)
public class DemoApplication implements WebMvcConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}

@Controller
class HomeController {

	@GetMapping
	public View home() {
		return JStachioModelView.of(new IndexPage(Closed.cwd()));
	}

	@HxRequest
	@GetMapping("/open/{path}")
	public View open(@PathVariable String path) {
		return JStachioModelView.of(new Closed(Node.decode(path)).doOpen());
	}

	@HxRequest
	@GetMapping("/close/{path}")
	public View close(@PathVariable String path) {
		return JStachioModelView.of(new Closed(Node.decode(path)));
	}

	@HxRequest
	@GetMapping("/file/{path}")
	public View file(@PathVariable String path) {
		return JStachioModelView.of(new Details(Node.decode(path)));
	}

}

@JStache(path = "file")
class FileIcon {
}

@JStache(path = "folder")
class FolderIcon {
}

@JStache(path = "caret")
class CaretIcon {
}

@JStache(path = "index")
class IndexPage {
	private Closed root;

	public IndexPage(Closed root) {
		this.root = root;
	}

	public Closed getRoot() {
		return root;
	}

	public void setRoot(Closed root) {
		this.root = root;
	}
}

class Node implements Comparable<Node> {
	private String name;
	private Node parent;

	public boolean isLeaf() {
		return true;
	}

	public int getLevel() {
		return parent == null ? 0 : ((Closed) parent).getLevel() + 1;
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

	public String getPath() {
		if (getParent() == null) {
			return getName();
		}
		return ((Closed) getParent()).getPath() + "/" + getName();
	}

	public String getEncoded() {
		return encode(getPath());
	}

	@Override
	public int compareTo(Node o) {
		return (this.isLeaf() != o.isLeaf()) ? this.isLeaf() ? 1 : -1 : this.name.compareTo(o.name);
	}

}

@JStache(path = "closed")
class Closed extends Node {

	public static Closed ROOT = new Closed("/");

	public static Closed cwd() {
		return new Closed(".");
	}

	public Open doOpen() {
		Open open = new Open(this);
		File top = new File(getPath());
		if (top.listFiles() == null) {
			return open;
		}
		for (File file : top.listFiles()) {
			if (file.isDirectory()) {
				open.add(new Closed(file.getName()));
			} else {
				open.add(new Node(file.getName()));
			}
		}
		return open;
	}

	private Set<Node> children = new TreeSet<>();

	public Closed(String name) {
		super(StringUtils.getFilename(name));
		String[] tokens = StringUtils.tokenizeToStringArray(name, "/");
		if (tokens.length <= 1) {
			return;
		}
		Closed folder = this;
		for (int i = tokens.length - 1; i-- > 0;) {
			folder.setParent(new Closed(tokens[i]));
			folder = (Closed) folder.getParent();
		}
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
		return false;
	}

	public Set<Node> getChildren() {
		return children;
	}
}

@JStache(path = "open")
class Open extends Closed {

	public Open(Closed closed) {
		super(closed.getName());
		setParent(closed.getParent());
	}

	@Override
	public boolean isOpen() {
		return true;
	}
}

@JStache(template = """
	<span class="popup" style="visibility:visible;">Name: {{name}}<br/>Size: {{size}}<br/>Modified: {{time}}</span>
	""")
class Details {

	private File file;

	public Details(String path) {
		this.file = new File(path);
	}

	public long size() {
		return this.file.length();
	}

	public String name() {
		return this.file.getName();
	}

	public LocalDateTime time() {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.file.lastModified()), TimeZone.getDefault().toZoneId());
	}
}