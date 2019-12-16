# alc enum-repls

## Purpose

Find information about local networked Clojure REPLs, notably port numbers.

Currently supported are socket, io-prepl (prepl), remote-prepl, nrepl, shadow-cljs, boot's socket repl.

## Prerequisites

### Running

* Some JDK (using >= 8 here) with `jcmd` (until https://github.com/oracle/graal/issues/1629 is addressed)
* clj / clojure, if not using the native-image binary

### Building

* Some JDK (using >= 8 here)
* [Native Image](https://www.graalvm.org/docs/reference-manual/aot-compilation/#install-native-image) (implies appropriate [GraalVM](https://github.com/oracle/graal) >= 19.3.0)
* Clojure 1.9 and its clj tool

## Quick Trial

Find unspecified REPLs:

```
$ clj -Sdeps '{:deps {alc.enum-repls {:git/url "https://github.com/sogaiu/alc.enum-repls" :sha "f6f7a9572b33f075f9fa7fa1ebcd6fd4bc6d4999"}}}' -m alc.enum-repls.main
```

Find REPLs for project with directory `/home/alice/clj-project`:

```
$ clj -Sdeps '{:deps {alc.enum-repls {:git/url "https://github.com/sogaiu/alc.enum-repls" :sha "f6f7a9572b33f075f9fa7fa1ebcd6fd4bc6d4999"}}}' -m alc.enum-repls.main /home/alice/clj-project
```

## Usage

Additionally, there are two more ways enum-repls can be used; via:

* an installed binary and
* a clj alias invocation

### Installed Binary

Find unspecified REPLs:

```
$ enum-repls
```

Find REPLs for project with directory `/home/alice/clj-project`:

```
$ enum-repls /home/alice/clj-project
```

### clj Alias Invocation

Edit the `:aliases` section of `~/.clojure/deps.edn` to contain:

```
   :enum-repls
   {
    :extra-deps {sogaiu/alc.enum-repls
                 {:git/url "https://github.com/sogaiu/alc.enum-repls"
                  :sha "f6f7a9572b33f075f9fa7fa1ebcd6fd4bc6d4999"
    :main-opts ["-m" "alc.enum-repls.main"]
   }
```

Note: the SHA value is unlikely to be the latest due to the properties of hash algorithms.

Find unspecified REPLs:

```
$ clj -A:enum-repls
```

Find REPLs for project with directory `/home/alice/clj-project`:

```
$ clj -A:find-repls /home/alice/clj-project
```

## Output

The output should be one line per detected networked REPL and has form:

```
<pid>\t<port>\t<type>\t<project-dir-path>
```

- `<pid>` is typically an integer
- `<port>` is typically a port number
- `<type>` is clojure-socket-repl | clojure-socket-prepl | clojure-remote-prepl | nrepl | shadow-cljs-repl | boot-socket-repl
- `<project-dir-path>` is a path (JVM user.dir) for a process of a project

Note:

- fields are tab-separated
- more than one REPL may exist for a given process
- REPL detection is not exhaustive (e.g. may not find manually started ones)
- info is obtained via `jcmd` and may not be up-to-date in some cases

## Building

* Clone this repository and cd to the clone

* Ensure:
  * native-image is on your PATH -OR-
  * Set GRAALVM_HOME appropriately (e.g. on Arch Linux this might be /usr/lib/jvm/java-8-graal)

* Build the native-image binary:

```
$ clj -A:native-image
```

After some time, this should produce a file named `enum-repls` in the current directory.

## Acknowledgments

Thanks to (at least) the following folks:

* borkdude
* lread
* mauricioszabo
* Saikyun
* seancorfield
* taylorwood
* thheller
