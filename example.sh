#!/bin/sh
#

# delete chunks that don't contain enchantment tables, railway, or chests
#
java -jar MCChunkDeleter.jar --worldFolder=world --blockIDs="27,28,54,66,116"
