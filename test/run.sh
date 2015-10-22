

ok=0
fail=0

for f in test/*/*.k; do
	java -ea -jar ./konoha.jar $f
	if [ $? -eq 0 ] ; then
		ok=$(( $ok+1 ))
		echo -n $'\e[33mOK'
	else
		fail=$(( $fail+1 ))
		echo -n $'\e[31mFAIL'
	fi
	echo -n " $f"
	echo $'\e[0m'
done

echo "RESULT pass $ok fail $fail"
