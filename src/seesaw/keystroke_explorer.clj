;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Window to learn about keystrokes and keycodes.  Run (start-explorer) to use."}    
  seesaw.keystroke-explorer
  (:use seesaw.core seesaw.keymap seesaw.mig seesaw.dev seesaw.keystroke
        )  
  (:import javax.swing.KeyStroke java.awt.event.KeyEvent))

(def cf (frame :title "to enter keycodes" :minimum-size [1700 :by 150]))

(def pressed-entry (text :minimum-size [70 :by 18]))

(def typed-entry (text :minimum-size [70 :by 18]))

(def released-entry (text :minimum-size [70 :by 18]))

(def code-display (text :minimum-size [70 :by 18]))

(def type-of-event (text :minimum-size [70 :by 18]))

(def keystroke-display (text :minimum-size [250 :by 18]))

(def keystroke-analyzed (text :minimum-size [180 :by 40] :multi-line? true))

(def cp (mig-panel :constraints ["wrap 2"]
                   :items [["enter to see key pressed"]
                           [pressed-entry]
                           ["enter to see key typed"]
                           [typed-entry]
                           ["enter to see key released"]
                           [released-entry]
                           ["event type"]
                           [type-of-event]
                           ["keycode"]
                           [code-display]
                           ["keystroke"]
                           [keystroke-display]
                           ["analyzed keystroke"]
                           [keystroke-analyzed "span" "growy" "growx"]    
                           ]))

(defn showf [f] (-> f show! pack!))

(defn display [f content]
  (config! f :content content)
  content)

(def pressed-listen (listen pressed-entry :key-pressed 
               (fn [e] (do 
                         (config! pressed-entry :text "")
                         (config! type-of-event :text (str (type e)))
                         (config! code-display :text
                                  (.. (KeyStroke/getKeyStrokeForEvent e) getKeyCode))
                         (config! keystroke-display :text
                                  (.. (KeyStroke/getKeyStrokeForEvent e) toString))
                         (config! keystroke-analyzed :text
                                  (str (analyze-keystroke (keystroke e))))))))

(def typed-listen (listen typed-entry :key-typed 
               (fn [e] (do 
                         (config! typed-entry :text "")
                         (config! type-of-event :text (str (type e)))
                         (config! code-display :text
                                  (.. (KeyStroke/getKeyStrokeForEvent e) getKeyCode))
                         (config! keystroke-display :text
                                  (.. (KeyStroke/getKeyStrokeForEvent e) toString))
                         (config! keystroke-analyzed :text
                                  (str (analyze-keystroke (keystroke e))))))))

(def released-listen (listen released-entry :key-released 
               (fn [e] (do 
                         (config! released-entry :text "")
                         (config! type-of-event :text (str (type e)))
                         (config! code-display :text
                                  (.. (KeyStroke/getKeyStrokeForEvent e) getKeyCode))
                         (config! keystroke-display :text
                                  (.. (KeyStroke/getKeyStrokeForEvent e) toString))
                         (config! keystroke-analyzed :text
                                  (str (analyze-keystroke (keystroke e))))))))
  

(defn start-explorer [] (do
                   (display cf cp)
                   (showf cf)))

