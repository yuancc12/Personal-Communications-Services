import React from "react";
import { TransitionSeries, linearTiming, springTiming } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { slide } from "@remotion/transitions/slide";
import { wipe } from "@remotion/transitions/wipe";
import { IntroScene } from "./scenes/IntroScene";
import { TaglineScene } from "./scenes/TaglineScene";
import { UserJourneyScene } from "./scenes/UserJourneyScene";
import { Feature1Scene } from "./scenes/Feature1Scene";
import { Feature2Scene } from "./scenes/Feature2Scene";
import { Feature3Scene } from "./scenes/Feature3Scene";
import { Feature4Scene } from "./scenes/Feature4Scene";
import { ResultsScene } from "./scenes/ResultsScene";
import { OutroScene } from "./scenes/OutroScene";

const TRANS = 18;

// Scene durations in frames (30fps)
// Total: 180+180+210+240+240+240+240+210+180 = 1920 − 8×18(144) = 1776 ≈ 59s
export const SCENE_DURATIONS = {
  intro: 180,       // 6s
  tagline: 180,     // 6s
  journey: 210,     // 7s
  feature1: 240,    // 8s
  feature2: 240,    // 8s
  feature3: 240,    // 8s
  feature4: 240,    // 8s
  results: 210,     // 7s
  outro: 180,       // 6s
};

const spring18 = springTiming({ config: { damping: 200 }, durationInFrames: TRANS });
const linear18 = linearTiming({ durationInFrames: TRANS });

export const SMSVoteIntro: React.FC = () => {
  return (
    <TransitionSeries>
      <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS.intro}>
        <IntroScene />
      </TransitionSeries.Sequence>

      <TransitionSeries.Transition presentation={fade()} timing={spring18} />

      <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS.tagline}>
        <TaglineScene />
      </TransitionSeries.Sequence>

      <TransitionSeries.Transition presentation={slide({ direction: "from-right" })} timing={linear18} />

      <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS.journey}>
        <UserJourneyScene />
      </TransitionSeries.Sequence>

      <TransitionSeries.Transition presentation={slide({ direction: "from-right" })} timing={linear18} />

      <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS.feature1}>
        <Feature1Scene />
      </TransitionSeries.Sequence>

      <TransitionSeries.Transition presentation={wipe({ direction: "from-right" })} timing={linear18} />

      <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS.feature2}>
        <Feature2Scene />
      </TransitionSeries.Sequence>

      <TransitionSeries.Transition presentation={slide({ direction: "from-right" })} timing={linear18} />

      <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS.feature3}>
        <Feature3Scene />
      </TransitionSeries.Sequence>

      <TransitionSeries.Transition presentation={slide({ direction: "from-right" })} timing={linear18} />

      <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS.feature4}>
        <Feature4Scene />
      </TransitionSeries.Sequence>

      <TransitionSeries.Transition presentation={wipe({ direction: "from-right" })} timing={linear18} />

      <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS.results}>
        <ResultsScene />
      </TransitionSeries.Sequence>

      <TransitionSeries.Transition presentation={fade()} timing={spring18} />

      <TransitionSeries.Sequence durationInFrames={SCENE_DURATIONS.outro}>
        <OutroScene />
      </TransitionSeries.Sequence>
    </TransitionSeries>
  );
};
