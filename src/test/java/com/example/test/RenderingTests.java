package com.example.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.jstach.jstache.JStache;
import io.jstach.jstache.JStachePartial;
import io.jstach.jstache.JStachePartials;
import io.jstach.jstachio.JStachio;

@JStachePartials(@JStachePartial(name = "foo", template = "<span>bar</span>"))
public class RenderingTests {

	@JStache(template = "{{>foo}}")
	static record Index() {
	}

	@JStache(template = "<div>{{>foo}}</div>")
	static record Wrapped() {
	}

	@Test
	void test() {
		assertThat(JStachio.render(new Index())).contains("<span>bar</span>");
		assertThat(JStachio.render(new Wrapped())).contains("<span>bar</span>");
	}

}
