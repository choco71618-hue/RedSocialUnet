package com.redsocial.ui.component;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;

public class Skeleton extends Composite<Div> {

    public Skeleton() {
        addClassName("skeleton");
    }

    public static Skeleton line() {
        return new Skeleton().withClass("skeleton-line");
    }

    public static Skeleton lineWide() {
        return new Skeleton().withClass("skeleton-line").withClass("skeleton-wide");
    }

    public static Skeleton circle() {
        return new Skeleton().withClass("skeleton-circle");
    }

    public static Skeleton block() {
        return new Skeleton().withClass("skeleton-block");
    }

    /** Card skeleton: avatar círculo + 2 líneas de texto. */
    public static Div card() {
        Div card = new Div();
        card.addClassName("skeleton-card");
        Skeleton circle = circle();
        circle.addClassName("skeleton-card-avatar");
        card.add(circle);
        Div body = new Div(lineWide(), line());
        body.addClassName("skel-body");
        card.add(body);
        return card;
    }

    public Skeleton withClass(String className) {
        addClassName(className);
        return this;
    }
}