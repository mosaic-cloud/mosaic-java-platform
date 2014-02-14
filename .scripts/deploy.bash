#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

if test "${_mosaic_deploy_artifactory:-false}" == true ; then
	case "${_pom_classifier}" in
		
		( component | *-component )
			env "${_mvn_env[@]}" "${_mvn_bin}" \
					-f "${_mvn_pom}" \
					--projects "${_pom_group}:${_pom_artifact}" \
					--also-make \
					"${_mvn_args[@]}" \
					deploy \
					-DskipTests=true \
					-D_mvn_skip_analyze=true \
					-D_mvn_skip_licenses=true \
					-D_mvn_skip_formatter=true
		;;
		
		( artifacts )
			# FIXME: We have to fix this...
			env "${_mvn_env[@]}" "${_mvn_bin}" \
					-f "${_mvn_pom}" \
					--also-make \
					"${_mvn_args[@]}" \
					deploy \
					-DskipTests=true \
					-D_mvn_skip_analyze=true \
					-D_mvn_skip_licenses=true \
					-D_mvn_skip_formatter=true
			exit 0
		;;
		
		( * )
			exit 1
		;;
	esac
fi

case "${_pom_classifier}" in
		( component | *-component )
		;;
		( artifacts )
			exit 0
		;;
		( * )
			exit 1
		;;
esac

if test "${_mosaic_deploy_cp:-false}" == true ; then
	test -n "${_mosaic_deploy_cp_store}"
	_mosaic_deploy_cp_target="${_mosaic_deploy_cp_store}/${_package_name}--${_package_version}.tar.gz"
	echo "[ii] deploying via \`cp\` method to \`${_mosaic_deploy_cp_target}\`..." >&2
	cp -T -- "${_outputs}/package.tar.gz" "${_mosaic_deploy_cp_target}"
fi

if test "${_mosaic_deploy_curl:-false}" == true ; then
	test -n "${_mosaic_deploy_curl_credentials}"
	test -n "${_mosaic_deploy_curl_store}"
	_mosaic_deploy_curl_target="${_mosaic_deploy_curl_store}/${_package_name}--${_package_version}.tar.gz"
	echo "[ii] deploying via \`curl\` method to \`${_mosaic_deploy_curl_target}\`..." >&2
	env -i "${_curl_env[@]}" "${_curl_bin}" "${_curl_args[@]}" \
			--anyauth --user "${_mosaic_deploy_curl_credentials}" \
			--upload-file "${_outputs}/package.tar.gz" \
			-- "${_mosaic_deploy_curl_target}"
fi

if test "${_mosaic_deploy_cook:-false}" == true ; then
	test -n "${_mosaic_deploy_cook_server}"
	echo "[ii] deploying via \`cook\` method to \`${_mosaic_deploy_cook_server}\`..." >&2
	env -i "${_ssh_env[@]}" "${_ssh_bin}" "${_ssh_args[@]}" \
			-T "${_mosaic_deploy_cook_server}" \
		<"${_outputs}/package.tar.gz"
fi

exit 0
