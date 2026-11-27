import React from "react";
import { Composition } from "remotion";
import { SMSVoteIntro, SCENE_DURATIONS } from "./SMSVoteIntro";

// 9 scenes × avg ~8.9s = 2520 frames − 8 transitions × 18 = 2520 − 144 = 2376 frames ≈ 79s
const TOTAL_FRAMES =
  Object.values(SCENE_DURATIONS).reduce((a, b) => a + b, 0) - 8 * 18;

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
