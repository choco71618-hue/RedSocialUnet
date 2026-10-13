package com.redsocial.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

@Tag("lottie-view")
@JsModule("./lottie-view.ts")
public class LottieView extends Component {

    public LottieView(String src) {
        getElement().setProperty("src", src);
    }
}