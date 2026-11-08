#version 330

uniform sampler2D CurrentSampler;
uniform sampler2D HistorySampler;

layout(std140) uniform MotionBlurConfig {
    float Decay;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 current = texture(CurrentSampler, texCoord);
    vec4 history = texture(HistorySampler, texCoord);
    fragColor = mix(current, history, Decay);
}
