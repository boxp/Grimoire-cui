(ns grimoire.raspberrypi
  (:import (com.pi4j.io.gpio GpioFactory RaspiPin PinState))
)
(def my-led (. RaspiPin -GPIO_01))

(defn init-outpin
  [raspi-pin]
  (-> GpioFactory
    (.getInstance)
    (.provisionDigitalOutputPin raspi-pin "MyLED" (. PinState -HIGH))))

