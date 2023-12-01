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
import io.jstach.jstache.JStachePartial;
import io.jstach.jstache.JStachePartials;
import io.jstach.jstache.JStachePath;
import io.jstach.opt.spring.webmvc.JStachioModelView;

@SpringBootApplication
@JStachePath(prefix = "templates/", suffix = ".mustache")
@JStacheFormatterTypes(types = LocalDateTime.class)
@JStachePartials({
		@JStachePartial(name = "caret", template = """
					<svg aria-hidden="true" focusable="false" data-icon="caret-down" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 512" class="icon-caret-down"><path fill="currentColor" d="M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z"></path></svg>
				"""),
		@JStachePartial(name = "file", template = """
					<span class="icon"><svg aria-hidden="true" focusable="false" data-icon="file" role="img" xmlns="http://www.w3.org/2000/svg" class="icon-file" viewBox="0 0 384 512"><path fill="currentColor" d="M369.9 97.9L286 14C277 5 264.8-.1 252.1-.1H48C21.5 0 0 21.5 0 48v416c0 26.5 21.5 48 48 48h288c26.5 0 48-21.5 48-48V131.9c0-12.7-5.1-25-14.1-34zM332.1 128H256V51.9l76.1 76.1zM48 464V48h160v104c0 13.3 10.7 24 24 24h104v288H48z"></path></svg></span>
				"""),
		@JStachePartial(name = "folder", template = """
					<span class="icon"><svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="folder" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" class="icon-folder"><path fill="currentColor" d="M464 128H272l-64-64H48C21.49 64 0 85.49 0 112v288c0 26.51 21.49 48 48 48h416c26.51 0 48-21.49 48-48V176c0-26.51-21.49-48-48-48z"></path></svg></span>
				""") })
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

@JStache(path = "index")
class IndexPage {
	private Open root;

	public IndexPage(Closed root) {
		this.root = root.doOpen();
	}

	public Open getRoot() {
		return root;
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
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.file.lastModified()),
				TimeZone.getDefault().toZoneId());
	}
}