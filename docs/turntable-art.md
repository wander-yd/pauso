# Layered turntable art — preview 3

Three square 1254px resources, generated with the built-in image tool from the approved dark turntable reference. Original generated PNGs remain preserved separately; app resources use WebP quality 94 with unchanged dimensions and transparent alpha.

- `turntable_chassis.webp`: fixed dark ink-green chassis, empty circular platter, warm grazing brass edge light, mounting socket upper right. No baked timer, music icon or tonearm.
- `turntable_disc.webp`: isolated complete overhead vinyl, fine grooves, blank dark olive paper label, diffuse dim light, transparent outside edge. No time or UI marks.
- `turntable_arm.webp`: isolated long bent brushed-bronze tonearm, circular mounting cap, small two-screw cartridge and stylus, transparent backdrop; darker toward mounting end.

Prompt set: recreate the approved reference's photographic black-green/antique-bronze materials, quiet warm grazing light and receding shadows. Generate chassis only, complete unoccluded record only, and tonearm only as independent assets, with no lettering, numbers or interface markings. Record must be circular for runtime rotation; tonearm keeps its upper-right-to-lower-left orientation.

`TurntableView` registers these layers to one 600-unit scene. Only vinyl rotates counterclockwise and receives the bounded once-per-revolution micro-wobble. A stationary light overlay supplies grazing reflections. Chassis and bearing are stationary; the arm lifts slightly for silence. Live timer/icon do not inherit scene tilt or record movement. The remaining-time arc stays independent.

Validation: source assets inspected, alpha preserved, desktop Canvas material/registration composition inspected (not an Android screenshot). Native compilation, existing motion regression and APK resource inclusion checked. Physical phone layout, frame rate and perceived wobble remain unverified. Three decoded bitmaps use approximately 18 MiB; do not decode per frame.
