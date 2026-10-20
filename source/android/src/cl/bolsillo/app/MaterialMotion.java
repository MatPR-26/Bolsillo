/*
 * Copyright 2020 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * Adapted for Bolsillo: bounded targets, shorter distances and cancellation.
 * See licenses/MaterialMotion-NOTICE.txt.
 */
package cl.bolsillo.app;
import android.animation.*;import android.view.*;
/** Adapted Material ScaleProvider/FadeThroughProvider factories, without AndroidX dependencies. */
final class MaterialMotion {
 static Animator scale(View view,float start,float end){final float originalX=view.getScaleX(),originalY=view.getScaleY();ObjectAnimator animator=ObjectAnimator.ofPropertyValuesHolder(view,PropertyValuesHolder.ofFloat(View.SCALE_X,originalX*start,originalX*end),PropertyValuesHolder.ofFloat(View.SCALE_Y,originalY*start,originalY*end));animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){view.setScaleX(originalX);view.setScaleY(originalY);}});return animator;}
 static Animator fadeThrough(View view){float original=view.getAlpha()==0?1:view.getAlpha();ValueAnimator animator=ValueAnimator.ofFloat(0,1);animator.addUpdateListener(a->{float progress=(float)a.getAnimatedValue();view.setAlpha(original*Math.max(0,Math.min(1,(progress-.35f)/.65f)));});animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){view.setAlpha(original);}});return animator;}
 static Animator appear(View view,boolean section,int direction){AnimatorSet set=new AnimatorSet();if(section)set.playTogether(scale(view,.985f,1),fadeThrough(view));else set.playTogether(ObjectAnimator.ofFloat(view,View.TRANSLATION_X,Glass.dp(view.getContext(),direction*14),0),ObjectAnimator.ofFloat(view,View.ALPHA,.55f,1));set.setDuration(section?220:190);set.setInterpolator(PageMotion.EASE);return set;}
 static void touch(View view){StateListAnimator state=new StateListAnimator();ObjectAnimator down=ObjectAnimator.ofPropertyValuesHolder(view,PropertyValuesHolder.ofFloat(View.SCALE_X,.985f),PropertyValuesHolder.ofFloat(View.SCALE_Y,.985f));down.setDuration(85);down.setInterpolator(PageMotion.EASE);ObjectAnimator rest=ObjectAnimator.ofPropertyValuesHolder(view,PropertyValuesHolder.ofFloat(View.SCALE_X,1),PropertyValuesHolder.ofFloat(View.SCALE_Y,1));rest.setDuration(150);rest.setInterpolator(PageMotion.EASE);state.addState(new int[]{android.R.attr.state_enabled,android.R.attr.state_pressed},down);state.addState(new int[]{},rest);view.setStateListAnimator(state);state.jumpToCurrentState();}
}
