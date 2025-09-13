#!/usr/bin/env bash
set -euo pipefail

# --- Count Kotlin LOC (ignores comments + blank lines) ---
count_loc_file() {
  awk '
    BEGIN{in_block=0;cl=0}
    FNR==1{in_block=0}
    {
      s=$0; out="";
      while (1) {
        if (in_block==1) {
          end=index(s,"*/");
          if (end) { s=substr(s,end+2); in_block=0; }
          else { s=""; break; }
        } else {
          start=index(s,"/*");
          if (start) { out=out substr(s,1,start-1); s=substr(s,start+2); in_block=1; }
          else { out=out s; break; }
        }
      }
      line=out;
      pos=index(line,"//");
      if (pos) { line=substr(line,1,pos-1); }
      gsub(/^[ \t\r\n]+/,"",line);
      gsub(/[ \t\r\n]+$/,"",line);
      if (length(line)>0) cl++;
    }
    END{print cl}
  ' "$1"
}

# --- Init counters ---
CLASS_LOC=0
TEST_LOC=0
CLASS_FILES=0
TEST_FILES=0
TOTAL_FILES=0

# --- Scan all Kotlin files safely ---
while IFS= read -r -d '' file; do
  TOTAL_FILES=$((TOTAL_FILES+1))
  loc=$(count_loc_file "$file")

  if [[ "$file" =~ (^|/)src/test/|(^|/)test/|Test\.kt$ ]]; then
    TEST_LOC=$((TEST_LOC+loc))
    TEST_FILES=$((TEST_FILES+1))
  else
    CLASS_LOC=$((CLASS_LOC+loc))
    CLASS_FILES=$((CLASS_FILES+1))
  fi
done < <(find . -type f -name '*.kt' \
  -not -path '*/.git/*' \
  -not -path '*/build/*' \
  -not -path '*/.gradle/*' \
  -not -path '*/out/*' \
  -print0)

# --- Print summary ---
printf "Kotlin LOC (approx, excludes blanks/comments)\n"
printf "  Classes: %d LOC across %d files\n" "$CLASS_LOC" "$CLASS_FILES"
printf "  Tests:   %d LOC across %d files\n" "$TEST_LOC" "$TEST_FILES"
printf "  Total:   %d LOC across %d files\n" "$((CLASS_LOC+TEST_LOC))" "$TOTAL_FILES"

