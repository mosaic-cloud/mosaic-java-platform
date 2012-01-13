#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

test -e "${_outputs}/package.tar.gz"

ssh -T cook@agent1.builder.mosaic.ieat.ro. <"${_outputs}/package.tar.gz"

exit 0
