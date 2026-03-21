package io.springlens.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LensConsoleController {

    @GetMapping({"/lens", "/lens/"})
    public String lensConsole() {
        return "forward:/lens/index.html";
    }
}
