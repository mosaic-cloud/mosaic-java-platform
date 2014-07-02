#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

if test ! -e "${_outputs}" ; then
	if test -L "${_outputs}" ; then
		_outputs_store="$( readlink -- "${_outputs}" )"
	else
		_outputs_store="${_temporary}/$( basename -- "${_workbench}" )--$( readlink -m -- "${_outputs}" | tr -d '\n' | md5sum -t | tr -d ' \n-' )"
		ln -s -T -- "${_outputs_store}" "${_outputs}"
	fi
	if test ! -e "${_outputs_store}" ; then
		mkdir -- "${_outputs_store}"
	fi
fi

case "${_pom_classifier}" in
	
	( component | *-component )
		# FIXME: Add `--offline` flag, if Maven has already downloaded required plugins. (See `mosaic-distribution`.)
		exec env -i "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_pom}" \
				--projects "${_pom_group}:${_pom_artifact}" \
				--also-make \
				--activate-profiles "${_mvn_profiles},do-dependency-update" \
				--update-snapshots \
				--fail-fast \
				"${_mvn_args[@]}" \
				initialize \
				-D_mvn_skip_all=true \
				-D_mvn_skip_target_ln=false \
				-D_mvn_skip_update=false
	;;
	
	( artifacts )
		# FIXME: Add `--offline` flag, if Maven has already downloaded required plugins. (See `mosaic-distribution`.)
		# FIXME: We have to fix this...
		exec env -i "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_pom}" \
				--also-make \
				--activate-profiles "${_mvn_profiles},do-dependency-update" \
				--update-snapshots \
				--fail-fast \
				"${_mvn_args[@]}" \
				initialize \
				-D_mvn_skip_all=true \
				-D_mvn_skip_target_ln=false \
				-D_mvn_skip_update=false
	;;
	
	( * )
		exit 1
	;;
esac

exit 1
