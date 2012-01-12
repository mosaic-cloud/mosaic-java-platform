#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

_mvn_args+=(
	package -DskipTests=true
)

exec env "${_mvn_env[@]}" "${_mvn_bin}" "${_mvn_args[@]}"
