import React from "react";
import { Composition } from "remotion";
import { SMSVoteIntro, SCENE_DURATIONS } from "./SMSVoteIntro";

// 10 scenes: sum − 9 transitions × 18
const TOTAL_FRAMES =
  Object.values(SCENE_DURATIONS).reduce((a, b) => a + b, 0) - 9 * 18;

export const Root: React.FC = () => {
  return (
    <Composition
      id="SMSVoteIntro"
      component={SMSVoteIntro}
      durationInFrames={TOTAL_FRAMES}
      fps={30}
      width={1920}
      height={1080}
    />
  );
};
