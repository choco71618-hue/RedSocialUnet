package com.redsocial.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

/**
 * Reproductor de video HTML5 nativo para las publicaciones.
 */
@Tag("video")
public class VideoPlayer extends Component {

    public VideoPlayer(String src) {
        getElement().setAttribute("src", src);
        getElement().setAttribute("controls", true);
        getElement().setAttribute("preload", "metadata");
        getElement().setAttribute("playsinline", true);
        addClassName("post-media");
        addClassName("post-video");
    }
}
