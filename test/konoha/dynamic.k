function indexGetter() {
    var a = null;
    a = [23, 32];
    var b = null;
    b = 0;
    println(a[b]);
}

function indexSetter() {
    var a = null;
    a = ["a", "b"];
    var b = null;
    b = 0;
    var c = null;
    c = "hello";
    /* a[b] = c; */
    println(a[b]);
}

function getter() {
    var a = null;
    a = [23, 32, 43];
    println(a.ArrayValues);
}

function setter() {
    var a = null;
    a = ["a", "de"];
    var b = null;
    b = ["5678", "132"];
    /* a.ArrayValues = b.ArrayValues; */
    println(a[0]);
}

indexGetter();
indexSetter();
getter();
setter();
