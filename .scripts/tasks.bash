#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

cat <<EOS

${_package_name}@requisites : \
		pallur-packages@java \
		pallur-packages@maven \
		pallur-environment

# FIXME: Move these to the requisites of 'mosaic-components-java-*'!
${_package_name}@requisites : \
		pallur-packages@jzmq

${_package_name}@prepare : ${_package_name}@requisites
	!exec ${_scripts}/prepare

${_package_name}@package : ${_package_name}@compile
	!exec ${_scripts}/package

${_package_name}@compile : ${_package_name}@prepare
	!exec ${_scripts}/compile

${_package_name}@publish : ${_package_name}@package
	!exec ${_scripts}/publish

EOS

exit 0
