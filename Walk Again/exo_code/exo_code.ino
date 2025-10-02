#define RHIP_LPWM 5
#define RHIP_RPWM 6

void setup() {
  pinMode(RHIP_LPWM, OUTPUT);
  pinMode(RHIP_RPWM, OUTPUT);

  // Move forward slowly
  analogWrite(RHIP_LPWM, 0);  // PWM 0–255
  analogWrite(RHIP_RPWM, 0);
}

void loop() {
  // Constant slow motion forward
}
