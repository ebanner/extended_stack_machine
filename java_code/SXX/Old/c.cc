#include <iostream>

main()
{
	int i;
	char c;

	cout << "i: " << flush;
	cin >> i;
	if (cin.fail())
		cout << "fail!" << endl;
	else
		cout << "integer=" << i << endl;
	cin.clear();
	cin >> c;
	if (cin.fail())
		cout << "fail!" << endl;
	else
		cout << "character=" << c << endl;

}
