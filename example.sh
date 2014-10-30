#!/bin/sh
#

# delete chunks that don't contain enchantment tables, railway, or chests
#
java -cp .:MCChunkDeleter-1.0.0.jar nl.joranderaaff.minecraft.chunkdeleter.Main \
  --worldFolder=world \
  --blockIDs="27,28,54,66,116"
