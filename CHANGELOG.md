# Changelog

## [0.6.6] - 2022-4-4
- Added Nullable annotations to card|barcode|face detection methods and payloads
- Fix for bitmap scale issues

## [0.6.0] - 2022-3-04

#### Added
- Added ID_PHOTO insight to provide feedback when no photo is detected on an ID

## [0.5.6] - 2022-2-16

#### Added
- Added ID backside detection for use with withEnhanceInfoExtraction
- Other stability fixes

## [0.5.1] - 2022-2-10

#### Added
- Cropping of IDs requires less memory
- Other stability fixes

## [0.5.0] - 2022-2-1

#### Added
- Ability to disable camera torch
- withEnhanceInfoExtraction added to ID detection (CardDetect)
- Tighter cropping of IDs that are detected
- 'move away' instruction when too close to ID
