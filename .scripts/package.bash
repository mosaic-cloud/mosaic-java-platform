#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

if test -e "${_outputs}/package" ; then
	chmod -R +w -- "${_outputs}/package"
	rm -R -- "${_outputs}/package"
fi
if test -e "${_outputs}/package.cpio.gz" ; then
	chmod +w -- "${_outputs}/package.cpio.gz"
	rm -- "${_outputs}/package.cpio.gz"
fi

case "${_pom_classifier}" in
	
	( component | *-component )
		# FIXME: Add `--offline` flag, if Maven has already downloaded required plugins. (See `mosaic-distribution`.)
		env -i "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_pom}" \
				--projects "${_pom_group}:${_pom_artifact}" \
				--also-make \
				--activate-profiles "${_mvn_profiles}" \
				"${_mvn_args[@]}" \
				package \
				-D_mvn_skip_all=true
	;;
	
	( artifacts )
		# FIXME: Add `--offline` flag, if Maven has already downloaded required plugins. (See `mosaic-distribution`.)
		# FIXME: We have to fix this...
		env -i "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_pom}" \
				--also-make \
				--activate-profiles "${_mvn_profiles}" \
				"${_mvn_args[@]}" \
				package \
				-D_mvn_skip_all=true
		exit 0
	;;
	
	( * )
		exit 1
	;;
esac

mkdir -- "${_outputs}/package"
mkdir -- "${_outputs}/package/bin"
mkdir -- "${_outputs}/package/lib"

mkdir -- "${_outputs}/package/lib/java"
find -H "${_outputs}/${_pom_group}--${_pom_artifact}--${_pom_version}/target" -type f -name "${_package_jar_name}" -exec cp -t "${_outputs}/package/lib/java" -- {} \;
find -H "${_tools}/lib" -xtype f \( -name 'lib*.so' -o -name 'lib*.so.*' \) -exec cp -t "${_outputs}/package/lib/java" -- {} \;

mkdir -- "${_outputs}/package/lib/scripts"

cat >"${_outputs}/package/lib/scripts/_do.sh" <<'EOS'
#!/bin/bash

set -e -E -u -o pipefail -o noclobber -o noglob +o braceexpand || exit 1
trap 'printf "[ee] failed: %s\n" "${BASH_COMMAND}" >&2' ERR || exit 1

_self_basename="$( basename -- "${0}" )"
_self_realpath="$( readlink -e -- "${0}" )"
cd "$( dirname -- "${_self_realpath}" )"
cd ../..
_package="$( readlink -e -- . )"
cmp -s -- "${_package}/lib/scripts/_do.sh" "${_self_realpath}"
test -e "${_package}/lib/scripts/${_self_basename}.bash"

test -d "${_package}/env/paths"
_PATH="$(
		find "${_package}/env/paths" -xdev -mindepth 1 -maxdepth 1 -type l -xtype d \
		| sort \
		| while read -r _path ; do
			printf ':%s' "$( readlink -m -- "${_path}" )"
		done
)"
_PATH="${_PATH/:}"
export PATH="${_PATH}"

if test -e "${_package}/env/variables" ; then
	while read -r _path ; do
		_name="$( basename -- "${_path}" )"
		case "${_name}" in
			( @a:* )
				test -L "${_path}"
				_name="${_name/*:}"
				_value="$( readlink -e -- "${_path}" )"
			;;
			( * )
				echo "[ee] invalid variable \`${_path}\`; aborting!"
				exit 1
			;;
		esac
		export -- "${_name}=${_value}"
	done < <(
			find "${_package}/env/variables" -xdev -mindepth 1 \
			| sort
	)
	
fi

_LD_LIBRARY_PATH="${_package}/lib/java:${LD_LIBRARY_PATH:-}"

_java_bin="$( PATH="${_PATH}" type -P -- java || true )"
if test -z "${_java_bin}" ; then
	echo "[ee] missing \`java\` (Java interpreter) executable in path: \`${_PATH}\`; ignoring!" >&2
	exit 1
fi

_java_jars="${_package}/lib/java"
_java_args=(
		-server
		"-Djava.library.path=${_LD_LIBRARY_PATH}"
)
_java_env=(
		PATH="${_PATH}"
		LD_LIBRARY_PATH="${_LD_LIBRARY_PATH}"
)

_package_jar_name='@package_jar_name@'

if test "${#}" -eq 0 ; then
	. "${_package}/lib/scripts/${_self_basename}.bash"
else
	. "${_package}/lib/scripts/${_self_basename}.bash" "${@}"
fi

echo "[ee] script \`${_self_main}\` should have exited..." >&2
exit 1
EOS

sed -r -e 's|@package_jar_name@|'"${_package_jar_name}"'|g' -i -- "${_outputs}/package/lib/scripts/_do.sh"

chmod +x -- "${_outputs}/package/lib/scripts/_do.sh"

for _script_name in "${_package_scripts[@]}" ; do
	test -e "${_scripts}/${_script_name}" || continue
	if test -e "${_scripts}/${_script_name}.bash" ; then
		_script_path="${_scripts}/${_script_name}.bash"
	else
		_script_path="$( dirname -- "$( readlink -e -- "${_scripts}/${_script_name}" )" )/${_script_name}.bash"
	fi
	cp -T -- "${_script_path}" "${_outputs}/package/lib/scripts/${_script_name}.bash"
	ln -s -T -- ./_do.sh "${_outputs}/package/lib/scripts/${_script_name}"
	cat >"${_outputs}/package/bin/${_package_name}--${_script_name}" <<EOS
#!/bin/bash
set -e -E -u -o pipefail -o noclobber -o noglob +o braceexpand || exit 1
trap 'printf "[ee] failed: %s\n" "\${BASH_COMMAND}" >&2' ERR || exit 1
if test "\${#}" -eq 0 ; then
	exec "\$( dirname -- "\$( readlink -e -- "\${0}" )" )/../lib/scripts/${_script_name}"
else
	exec "\$( dirname -- "\$( readlink -e -- "\${0}" )" )/../lib/scripts/${_script_name}" "\${@}"
fi
EOS
	chmod +x -- "${_outputs}/package/bin/${_package_name}--${_script_name}"
done

chmod -R a+rX-w,u+w -- "${_outputs}/package"

cd "${_outputs}/package"
find . \
		-xdev -depth \
		\( -type d -o -type l -o -type f \) \
		-print0 \
| cpio -o -H newc -0 --quiet \
| gzip --fast >"${_outputs}/package.cpio.gz"

if test -n "${_artifacts_cache}" ; then
	cp -T -- "${_outputs}/package.cpio.gz" "${_artifacts_cache}/${_package_name}--${_package_version}.cpio.gz"
fi

exit 0
