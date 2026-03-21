package io.springlens.spi;

import java.util.List;

public interface SkillGenerator {

    List<GeneratedSkillTool> generate(SkillGenerationRequest request);
}
