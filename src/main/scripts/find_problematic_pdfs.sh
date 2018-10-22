#!/bin/bash
# This (simple) script is designed to help find problems with a large number of downloaded PDFs.
# This is will accept any number of directories, representing the downloaded SAF data, that are within a single top-level directory.
# This script requires the following programs: bash, awk, sed, file, md5sum.
#
# A log file will be generated, designating Invalid or Missing PDFs.
# A checksum directory will be created, which will contain files used to designate duplicate PDFs based on their md5 checksum.
# The checksum files within that directoy only reference the duplicates.
#   (That is to say, if document-1.pdf and document-2.pdf have identical checksums, then only document-2.pdf will be added to the checksum file).
#   The checksum directories themselves are named after their appropriate SAF directory as passed to this script.

main() {
  local contents_file="contents"
  local bundle_name="bundle:ORIGINAL"
  local log_file="analysis.log"
  local checksum_directory="checksums"
  local directories="$1"
  local directory=
  local pdfs=
  local top=$PWD
  local write_directory=$top

  if [[ $directories == "" ]] ; then
    write_log "ERROR" "No directories specified."
    return 1
  fi

  for directory in $directories ; do
    cd $top
    echo
    echo "Processing directory '$directory'"

    if [[ ! -d "$directory" ]] ; then
      write_log "WARNING" "Invalid Directory: '$directory'."
      continue
    fi

    if [[ ! -f "$directory/$contents_file" ]] ; then
      write_log "WARNING" "Missing Contents File: '$directory/$contents_file'."
      continue
    fi

    cd $top/$directory
    pdfs=$(awk "/$bundle_name/" $contents_file | sed -e "s|[[:space:]]*$bundle_name\$||g")

    if [[ $pdfs == "" ]] ; then
      write_log "WARNING" "Empty Contents File: '$directory/$contents_file'."
      continue
    fi

    validate_pdfs
  done

  cd $top

  return 0
}

validate_pdfs() {
  local pdf=
  local mime=
  local checksum=

  declare -a checksums

  for pdf in $pdfs ; do
    echo " - PDF '$pdf'"

    if [[ ! -f $pdf ]] ; then
      write_log "ERROR" "Missing PDF: '$directory/$pdf'."
      continue
    fi

    mime=$(file -b -i "$pdf" | sed -e 's|;.*$||')
    if [[ $mime != "application/pdf" ]] ; then
      write_log "ERROR" "Invalid PDF: '$directory/$pdf'."
      continue
    fi

    checksum=$(md5sum $pdf | sed -e 's|[[:space:]].*$||')
    if [[ $checksum == "" ]] ; then
      write_log "WARNING" "Checksum Failed: '$directory/$pdf'."
      continue
    fi

    # note: xx is prepended to checksum to prevent bash from interpreting the checksum as an integer.
    if [[ ${checksums["xx$checksum"]} == "$checksum" ]] ; then
      checksum_log
    else
      checksums["xx$checksum"]="$checksum"
    fi
  done
}

checksum_log() {
  if [[ ! -d $write_directory$checksum_directory ]] ; then
    mkdir -p $write_directory$checksum_directory
  fi

  echo "$pdf  $checksum" >> $write_directory$checksum_directory/${directory}.duplicates
}

write_log() {
  local severity="$1"
  local message="$2" 

  echo "$severity: $message"
  echo "$message" >> $write_directory/$log_file
}

main "$*"
